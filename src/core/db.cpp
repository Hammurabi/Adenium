#include "db.h"
#include <iostream>
#include <leveldb/db.h>

LevelDB::LevelDB(const std::string &_Dbpath)
{
    leveldb::Options options;
    options.create_if_missing = true;
    leveldb::Status status = leveldb::DB::Open(options, _Dbpath, &_Db);
    if (!status.ok()) {
        throw std::runtime_error("Failed to open LevelDB: " + status.ToString());
    }
}

LevelDB::~LevelDB()
{
    this->Close();
}

void LevelDB::Put(const bytes &Key, const bytes& Value)
{
    _Db->Put(leveldb::WriteOptions(), Key.to_string(), Value.to_string());
}

bool LevelDB::Get(const bytes &Key, bytes* Value)
{
    std::string ReturnedValue = "";
    leveldb::Status status = _Db->Get(leveldb::ReadOptions(), Key.to_string(), &ReturnedValue);
    *Value = ReturnedValue;
    if (!status.ok()) {
        if (status.IsNotFound()) {
            return false; // Key not found
        } else {
            throw std::runtime_error("Failed to get value: " + status.ToString());
        }
    }

    return true;
}

void LevelDB::Delete(const bytes Key)
{
    leveldb::Status status = _Db->Delete(leveldb::WriteOptions(), Key.to_string());
    if (!status.ok()) {
        throw std::runtime_error("Failed to delete value: " + status.ToString());
    }
}

void Storage::Flush()
{
}

void LevelDB::Close()
{
    delete _Db;
    _Db = nullptr;
}

void DummyStorage::Put(const bytes &Key, const bytes &Value)
{
    _Storage[Key] = Value;
}

bool DummyStorage::Get(const bytes &Key, bytes *Value)
{
    auto it = _Storage.find(Key);
    if (it != _Storage.end()) {
        *Value = it->second;
        return true;
    }
    return false;
}

void DummyStorage::Delete(const bytes Key)
{
    auto it = _Storage.find(Key);
    if (it != _Storage.end()) {
        _Storage.erase(it);
    }
}

StorageCacheWrapper::StorageCacheWrapper(Storage *Db)
    : _Db(Db)
{
}

StorageCacheWrapper::~StorageCacheWrapper()
{
}

void StorageCacheWrapper::Put(const bytes &Key, const bytes &Value)
{
    _Cache[Key] = Value;
    _Deletes.erase(Key);
}

bool StorageCacheWrapper::Get(const bytes &Key, bytes *Value)
{
    if (_Deletes.find(Key) != _Deletes.end()) return false;
    auto Find = _Cache.find(Key);
    if (Find != _Cache.end()) {
        *Value = Find->second;
        return true;
    } else {
        return _Db->Get(Key, Value);
    }
}

void StorageCacheWrapper::Delete(const bytes Key)
{
    _Deletes.insert(Key);
    _Cache.erase(Key);
}

void StorageCacheWrapper::Flush()
{
    for (auto& Key : _Deletes) _Db->Delete(Key);
    for (const auto& Key : _Cache) _Db->Put(Key.first, _Cache[Key.first]);

    _Cache.clear();
    _Deletes.clear();
}

void DummyStorage::Display()
{
    for (const auto &pair : _Storage) {
        std::cout << pair.first.to_string() << ": " << pair.second.to_string() << std::endl;
    }
}

void Storage::Display()
{
}

StorageWrapper::StorageWrapper(Storage *Db, const bytes &Prefix)
    : _Db(Db), _Prefix(Prefix)
{
}

StorageWrapper::~StorageWrapper()
{
}

void StorageWrapper::Put(const bytes &Key, const bytes &Value)
{
    _Db->Put(_Prefix + Key, Value);
}

bool StorageWrapper::Get(const bytes &Key, bytes *Value)
{
    return _Db->Get(_Prefix + Key, Value);
}

void StorageWrapper::Delete(const bytes Key)
{
    _Db->Delete(_Prefix + Key);
}

void StorageWrapper::Display()
{
}
